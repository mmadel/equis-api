package com.equestrian.equis.service.riders;

import com.equestrian.equis.dto.accounts.TrainerRef;
import com.equestrian.equis.dto.riders.AdminRiderRow;
import com.equestrian.equis.entity.accounts.TrainerStatus;
import com.equestrian.equis.entity.assessment.BookingStatus;
import com.equestrian.equis.repository.riders.AdminRiderView;
import com.equestrian.equis.repository.riders.RiderRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

// The one place W3 rows are built, from the flat view a single query loads.
@Component
public class AdminRiderRowAssembler {
    private final RiderRepository riders;

    public AdminRiderRowAssembler(RiderRepository riders) {
        this.riders = riders;
    }

    // The updated rows the trainer changes answer with. Unknown ids are absent.
    public List<AdminRiderRow> rowsByIds(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return riders.findAdminViewByIds(BookingStatus.PLACED, ids).stream().map(this::row).toList();
    }

    public AdminRiderRow row(AdminRiderView v) {
        TrainerRef trainer = ref(v.trainerId(), v.trainerName(), v.trainerStatus());
        TrainerRef assessor = ref(v.assessorId(), v.assessorName(), v.assessorStatus());
        return new AdminRiderRow(v.id(), v.name(), v.phone(), v.levelId(), trainer, assessor,
                HowAssigned.of(trainer, assessor).value(), canResetToAssessor(trainer, assessor), v.createdAt());
    }

    // Only to an active assessor, and only when the rider is with someone else or nobody.
    private static boolean canResetToAssessor(TrainerRef trainer, TrainerRef assessor) {
        return assessor != null && "active".equals(assessor.status())
                && (trainer == null || !trainer.id().equals(assessor.id()));
    }

    private static TrainerRef ref(UUID id, String name, TrainerStatus status) {
        return id == null ? null : new TrainerRef(id, name, status.name().toLowerCase(Locale.ROOT));
    }
}
