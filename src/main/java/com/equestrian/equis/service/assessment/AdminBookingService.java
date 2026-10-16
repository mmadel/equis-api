package com.equestrian.equis.service.assessment;

import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ApiFieldError;
import com.equestrian.equis.common.error.ErrorCodes;
import com.equestrian.equis.dto.assessment.AdminBookingList;
import com.equestrian.equis.dto.assessment.AdminBookingRow;
import com.equestrian.equis.dto.assessment.BookingTotals;
import com.equestrian.equis.repository.assessment.AssessmentBookingRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

// W2: every member's latest assessment booking. Status and search are applied here, not in SQL,
// because "waiting" depends on the clock.
@Service
public class AdminBookingService {
    private static final Set<String> STATUSES = Set.of("upcoming", "waiting", "placed", "noshow", "cancelled");
    // Waiting first (they need action), then upcoming, then no-show and cancelled, then placed.
    private static final Map<String, Integer> GROUP =
            Map.of("waiting", 0, "upcoming", 1, "noshow", 2, "cancelled", 2, "placed", 3);
    private static final Comparator<LocalDateTime> SOONEST_FIRST = Comparator.naturalOrder();
    private static final Comparator<LocalDateTime> LATEST_FIRST = Comparator.reverseOrder();
    // Waiting and upcoming: soonest slot first. No-show and cancelled: latest slot first. Placed: latest result first.
    private static final Comparator<AdminBookingRow> LIST_ORDER = Comparator
            .<AdminBookingRow>comparingInt(r -> GROUP.get(r.status()))
            .thenComparing((x, y) -> switch (x.status()) {
                case "waiting", "upcoming" -> SOONEST_FIRST.compare(slotStart(x), slotStart(y));
                case "noshow", "cancelled" -> LATEST_FIRST.compare(slotStart(x), slotStart(y));
                default -> y.recordedAt().compareTo(x.recordedAt());
            })
            .thenComparing(r -> r.member().name(), String.CASE_INSENSITIVE_ORDER);

    private final AssessmentBookingRepository bookings;
    private final AdminBookingRowAssembler rows;
    private final Clock clock;

    public AdminBookingService(AssessmentBookingRepository bookings, AdminBookingRowAssembler rows, Clock clock) {
        this.bookings = bookings;
        this.rows = rows;
        this.clock = clock;
    }

    // status: null or blank = all. q: part of the name, or digits of the phone; blank = no search.
    // The totals always cover every row, whatever the filter.
    @Transactional(readOnly = true)
    public AdminBookingList list(String status, String q) {
        String wanted = parseStatus(status);
        List<AdminBookingRow> all = rows.rows(bookings.findLatestPerMember(), clock.instant());

        BookingTotals totals = new BookingTotals(count(all, "upcoming"), count(all, "waiting"), count(all, "placed"),
                count(all, "noshow"), count(all, "cancelled"));
        List<AdminBookingRow> shown = all.stream()
                .filter(r -> wanted == null || r.status().equals(wanted))
                .filter(matches(q))
                .sorted(LIST_ORDER)
                .toList();
        return new AdminBookingList(totals, shown);
    }

    private static String parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        if (!STATUSES.contains(status)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_FAILED, "Some fields are not valid.",
                    Map.of("fieldErrors", List.of(new ApiFieldError("status", ErrorCodes.INVALID_STATUS))));
        }
        return status;
    }

    // The name contains q, ignoring case; or, when q has digits, the phone contains those digits.
    private static Predicate<AdminBookingRow> matches(String q) {
        if (q == null || q.isBlank()) {
            return r -> true;
        }
        String text = q.trim().toLowerCase(Locale.ROOT);
        String digits = q.replaceAll("\\D", "");
        return r -> r.member().name().toLowerCase(Locale.ROOT).contains(text)
                || (!digits.isEmpty() && r.member().phone() != null && r.member().phone().contains(digits));
    }

    private static LocalDateTime slotStart(AdminBookingRow r) {
        return r.slot().date().atTime(r.slot().start());
    }

    private static long count(List<AdminBookingRow> rows, String status) {
        return rows.stream().filter(r -> r.status().equals(status)).count();
    }
}
