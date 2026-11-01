package com.equestrian.equis.service.riderapp;

import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ErrorCodes;
import com.equestrian.equis.dto.riderapp.ExpiringSoon;
import com.equestrian.equis.dto.riderapp.RiderAssessment;
import com.equestrian.equis.dto.riderapp.RiderBanners;
import com.equestrian.equis.dto.riderapp.RiderMe;
import com.equestrian.equis.dto.riderapp.RiderPackage;
import com.equestrian.equis.dto.riderapp.RiderPayment;
import com.equestrian.equis.dto.riderapp.RiderPayments;
import com.equestrian.equis.dto.riderapp.RiderProgress;
import com.equestrian.equis.dto.riderapp.RiderSession;
import com.equestrian.equis.dto.riderapp.RiderSessionEvaluation;
import com.equestrian.equis.dto.riderapp.RiderSessionPage;
import com.equestrian.equis.dto.riderapp.StarTrendPoint;
import com.equestrian.equis.entity.assessment.AssessmentBooking;
import com.equestrian.equis.entity.payments.Payment;
import com.equestrian.equis.entity.riders.Rider;
import com.equestrian.equis.entity.sessions.Session;
import com.equestrian.equis.repository.assessment.AssessmentBookingRepository;
import com.equestrian.equis.repository.evaluations.SessionEvaluationRepository;
import com.equestrian.equis.repository.evaluations.StarTrendRow;
import com.equestrian.equis.repository.payments.PaymentRepository;
import com.equestrian.equis.repository.payments.RiderSessionRow;
import com.equestrian.equis.repository.payments.SessionUsageRepository;
import com.equestrian.equis.repository.riders.RiderRepository;
import com.equestrian.equis.service.payments.PackageBalance;
import com.equestrian.equis.service.payments.RiderBalance;
import com.equestrian.equis.service.payments.RiderBalanceService;
import com.equestrian.equis.service.riders.LevelHistoryQuery;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

// What the rider app reads. Every method takes the rider id from the token, so a rider only ever sees their own data.
@Service
@Transactional(readOnly = true)
public class RiderAppQueryService {
    // Packages that expire this many days from today, or sooner, raise the Home warning.
    static final int EXPIRING_WITHIN_DAYS = 7;
    // The star trend covers this many months, the current one included.
    static final int TREND_MONTHS = 12;

    private final RiderRepository riders;
    private final RiderBalanceService balances;
    private final SessionUsageRepository usages;
    private final SessionEvaluationRepository evaluations;
    private final PaymentRepository payments;
    private final AssessmentBookingRepository bookings;
    private final LevelHistoryQuery levelHistory;
    private final ZoneId schoolZone;

    public RiderAppQueryService(RiderRepository riders, RiderBalanceService balances, SessionUsageRepository usages,
                                SessionEvaluationRepository evaluations, PaymentRepository payments,
                                AssessmentBookingRepository bookings, LevelHistoryQuery levelHistory,
                                @Value("${app.time-zone}") ZoneId schoolZone) {
        this.riders = riders;
        this.balances = balances;
        this.usages = usages;
        this.evaluations = evaluations;
        this.payments = payments;
        this.bookings = bookings;
        this.levelHistory = levelHistory;
        this.schoolZone = schoolZone;
    }

    public RiderMe me(UUID riderId) {
        Rider rider = rider(riderId);
        LocalDate today = balances.today();
        RiderBalance balance = balances.balanceOn(riderId, today);

        List<RiderPackage> packages = balance.packages().stream().map(RiderAppQueryService::toPackage).toList();
        RiderBanners banners = new RiderBanners(expiringSoon(balance.packages(), today), balance.sessionsLeft() == 0);
        // A disabled trainer's riders still show that trainer, as W3 does.
        String trainerName = rider.getTrainer() == null ? null : rider.getTrainer().getName();

        return new RiderMe(rider.getId(), rider.getName(), rider.getPhone(), rider.getEmail(),
                rider.getLevel().getId(), trainerName, balance.status(), balance.sessionsLeft(),
                balance.nextExpiry(), packages, banners);
    }

    // Two queries, the page and the count. No rider lookup: every rider request already checks that the token's
    // rider exists and is active (AuthUserConverter), so a request costs three queries in all.
    public RiderSessionPage sessions(UUID riderId, int page, int size) {
        List<RiderSession> items = usages.findRiderSessions(riderId, PageRequest.of(page, size)).stream()
                .map(RiderAppQueryService::toSession)
                .toList();
        return new RiderSessionPage(items, page, size, usages.countByRiderId(riderId));
    }

    public RiderAssessment assessment(UUID riderId) {
        Rider rider = rider(riderId);
        // Seed riders have no member, so no assessment.
        AssessmentBooking booking = (rider.getMemberId() == null ? null
                : bookings.findPlacedByMemberId(rider.getMemberId()).orElse(null));
        if (booking == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, ErrorCodes.ASSESSMENT_NOT_FOUND,
                    "There is no assessment for this rider.");
        }
        Session slot = booking.getSession();
        return new RiderAssessment(slot.getSessionDate(), slot.getStartTime(), slot.getEndTime(),
                booking.getAssessor().getName(), booking.getLevel().getId(), booking.getNotes());
    }

    // One query; the rider was checked with the token, as for sessions.
    public RiderPayments payments(UUID riderId) {
        return new RiderPayments(payments.findByRiderIdAndVoidedAtIsNullOrderByRecordedAtDesc(riderId).stream()
                .map(this::toPayment)
                .toList());
    }

    public RiderProgress progress(UUID riderId) {
        Rider rider = rider(riderId);
        LocalDate from = YearMonth.from(balances.today()).minusMonths(TREND_MONTHS - 1L).atDay(1);
        List<StarTrendPoint> trend = evaluations.starTrend(riderId, from).stream()
                .map(RiderAppQueryService::toPoint)
                .toList();
        return new RiderProgress(trend, levelHistory.history(rider));
    }

    // Packages with sessions left that expire within 7 days: their sessions together, and the earliest date.
    static ExpiringSoon expiringSoon(List<PackageBalance> packages, LocalDate today) {
        LocalDate limit = today.plusDays(EXPIRING_WITHIN_DAYS);
        List<PackageBalance> soon = packages.stream()
                .filter(p -> p.left() > 0 && !p.validUntil().isAfter(limit))
                .toList();
        if (soon.isEmpty()) {
            return null;
        }
        int sessions = soon.stream().mapToInt(PackageBalance::left).sum();
        LocalDate date = soon.stream().map(PackageBalance::validUntil).min(Comparator.naturalOrder()).orElseThrow();
        return new ExpiringSoon(sessions, date);
    }

    // Can't really fail: the token's rider was checked on this request. Kept so a miss is a 404, never a 500.
    private Rider rider(UUID riderId) {
        return riders.findById(riderId).orElseThrow(RiderAppQueryService::riderNotFound);
    }

    private static ApiException riderNotFound() {
        return new ApiException(HttpStatus.NOT_FOUND, ErrorCodes.RIDER_NOT_FOUND, "This rider does not exist.");
    }

    private static RiderPackage toPackage(PackageBalance p) {
        return new RiderPackage(p.paymentId(), YearMonth.from(p.startMonth()), p.validUntil(), p.sessions(),
                p.used(), p.left());
    }

    private static RiderSession toSession(RiderSessionRow r) {
        RiderSessionEvaluation evaluation = r.stars() == null ? null
                : new RiderSessionEvaluation(r.stars(), r.comment(), r.evaluatedAt());
        return new RiderSession(r.sessionId(), r.date(), r.start(), r.end(), r.levelId(), r.trainerName(),
                evaluation);
    }

    private RiderPayment toPayment(Payment p) {
        return new RiderPayment(p.getId(), LocalDate.ofInstant(p.getRecordedAt(), schoolZone), p.getAmountEgp(),
                p.getMethod(), YearMonth.from(p.getStartMonth()), p.getMonths(), p.getSessions(), p.getValidUntil());
    }

    private static StarTrendPoint toPoint(StarTrendRow r) {
        BigDecimal average = BigDecimal.valueOf(r.averageStars()).setScale(1, RoundingMode.HALF_UP);
        return new StarTrendPoint(YearMonth.of(r.year(), r.month()), average, r.evaluations());
    }
}
