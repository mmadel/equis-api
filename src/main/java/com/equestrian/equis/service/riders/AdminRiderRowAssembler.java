package com.equestrian.equis.service.riders;

import com.equestrian.equis.dto.accounts.TrainerRef;
import com.equestrian.equis.dto.payments.BalanceDto;
import com.equestrian.equis.dto.riders.AdminRiderRow;
import com.equestrian.equis.entity.accounts.TrainerStatus;
import com.equestrian.equis.entity.assessment.BookingStatus;
import com.equestrian.equis.repository.riders.AdminRiderView;
import com.equestrian.equis.repository.riders.RiderRepository;
import com.equestrian.equis.service.payments.RiderBalance;
import com.equestrian.equis.service.payments.RiderBalanceService;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

// The one place W3 rows are built, from the flat view a single query loads. Balances come in one batch, so a list
// costs the same number of queries whatever the number of riders.
@Component
public class AdminRiderRowAssembler {
    private final RiderRepository riders;
    private final RiderBalanceService balances;

    public AdminRiderRowAssembler(RiderRepository riders, RiderBalanceService balances) {
        this.riders = riders;
        this.balances = balances;
    }

    // The updated rows the trainer changes answer with. Unknown ids are absent.
    public List<AdminRiderRow> rowsByIds(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return rows(riders.findAdminViewByIds(BookingStatus.PLACED, ids));
    }

    // Each row with the rider's balance for today.
    public List<AdminRiderRow> rows(List<AdminRiderView> views) {
        if (views.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = views.stream().map(AdminRiderView::id).toList();
        Map<UUID, RiderBalance> byRider = balances.balancesOn(ids, balances.today());
        return views.stream().map(v -> row(v, BalanceDto.of(byRider.get(v.id())))).toList();
    }

    // How the rider got their trainer, without building the row (no balance needed).
    public String howAssigned(AdminRiderView v) {
        return HowAssigned.of(trainer(v), assessor(v)).value();
    }

    private static AdminRiderRow row(AdminRiderView v, BalanceDto balance) {
        TrainerRef trainer = trainer(v);
        TrainerRef assessor = assessor(v);
        return new AdminRiderRow(v.id(), v.name(), v.phone(), v.levelId(), trainer, assessor,
                HowAssigned.of(trainer, assessor).value(), canResetToAssessor(trainer, assessor), v.createdAt(),
                balance);
    }

    // Only to an active assessor, and only when the rider is with someone else or nobody.
    private static boolean canResetToAssessor(TrainerRef trainer, TrainerRef assessor) {
        return assessor != null && "active".equals(assessor.status())
                && (trainer == null || !trainer.id().equals(assessor.id()));
    }

    private static TrainerRef trainer(AdminRiderView v) {
        return ref(v.trainerId(), v.trainerName(), v.trainerStatus());
    }

    private static TrainerRef assessor(AdminRiderView v) {
        return ref(v.assessorId(), v.assessorName(), v.assessorStatus());
    }

    private static TrainerRef ref(UUID id, String name, TrainerStatus status) {
        return id == null ? null : new TrainerRef(id, name, status.name().toLowerCase(Locale.ROOT));
    }
}
