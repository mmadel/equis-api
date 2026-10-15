package com.equestrian.equis.service.riders;

import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ErrorCodes;
import com.equestrian.equis.dto.riders.Direction;
import com.equestrian.equis.dto.riders.LevelChangeRequest;
import com.equestrian.equis.entity.accounts.Trainer;
import com.equestrian.equis.entity.levels.Level;
import com.equestrian.equis.entity.riders.Rider;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LevelChangeRulesTest {
    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    private final LevelChangeRules rules = new LevelChangeRules();
    private final UUID ahmedId = UUID.randomUUID();

    @BeforeAll
    static void startValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void ownRiderPassesAndAnotherTrainersOrUnassignedRiderIsNotFound() {
        assertThatCode(() -> rules.requireOwnRider(rider(3, ahmedId), ahmedId)).doesNotThrowAnyException();

        assertRefused(() -> rules.requireOwnRider(rider(3, UUID.randomUUID()), ahmedId),
                HttpStatus.NOT_FOUND, ErrorCodes.RIDER_NOT_FOUND);
        assertRefused(() -> rules.requireOwnRider(rider(3, null), ahmedId),
                HttpStatus.NOT_FOUND, ErrorCodes.RIDER_NOT_FOUND);
    }

    @Test
    void expectedLevelMustBeTheRidersCurrentLevel() {
        Rider rider = rider(3, ahmedId);

        assertThatCode(() -> rules.requireExpectedLevel(rider, 3)).doesNotThrowAnyException();
        assertRefused(() -> rules.requireExpectedLevel(rider, 2), HttpStatus.CONFLICT, ErrorCodes.LEVEL_CHANGED);
    }

    @Test
    void targetIsOneStepAwayInsideTheLadder() {
        assertThat(rules.targetLevel(rider(5, ahmedId), Direction.UP)).isEqualTo(6);
        assertThat(rules.targetLevel(rider(2, ahmedId), Direction.DOWN)).isEqualTo(1);

        assertRefused(() -> rules.targetLevel(rider(6, ahmedId), Direction.UP),
                HttpStatus.CONFLICT, ErrorCodes.LEVEL_AT_TOP);
        assertRefused(() -> rules.targetLevel(rider(1, ahmedId), Direction.DOWN),
                HttpStatus.CONFLICT, ErrorCodes.LEVEL_AT_BOTTOM);
    }

    @Test
    void validRequestHasNoViolationsAndTheReasonIsTrimmed() {
        LevelChangeRequest req = new LevelChangeRequest(Direction.UP, 3, "  Ready for canter ");

        assertThat(validator.validate(req)).isEmpty();
        assertThat(req.reason()).isEqualTo("Ready for canter");
    }

    @Test
    void reasonMustBePresentNotBlankAndAtMost100Characters() {
        assertViolationsOn(new LevelChangeRequest(Direction.UP, 3, null), "reason");
        assertViolationsOn(new LevelChangeRequest(Direction.UP, 3, "   "), "reason");
        assertViolationsOn(new LevelChangeRequest(Direction.UP, 3, "x".repeat(101)), "reason");
        assertThat(validator.validate(new LevelChangeRequest(Direction.UP, 3, "x".repeat(100)))).isEmpty();
    }

    @Test
    void fromLevelMustBeBetween1And6AndDirectionIsRequired() {
        assertViolationsOn(new LevelChangeRequest(Direction.UP, 0, "Reason"), "fromLevelId");
        assertViolationsOn(new LevelChangeRequest(Direction.UP, 7, "Reason"), "fromLevelId");
        assertViolationsOn(new LevelChangeRequest(Direction.UP, null, "Reason"), "fromLevelId");
        assertViolationsOn(new LevelChangeRequest(null, 3, "Reason"), "direction");
    }

    private void assertViolationsOn(LevelChangeRequest req, String field) {
        assertThat(validator.validate(req))
                .extracting(ConstraintViolation::getPropertyPath)
                .extracting(Object::toString)
                .containsOnly(field);
    }

    private void assertRefused(Runnable call, HttpStatus status, String code) {
        assertThatThrownBy(call::run).isInstanceOfSatisfying(ApiException.class, ex -> {
            assertThat(ex.status()).isEqualTo(status);
            assertThat(ex.code()).isEqualTo(code);
        });
    }

    private static Rider rider(int levelId, UUID trainerId) {
        Level level = new Level();
        level.setId((short) levelId);
        Rider rider = new Rider();
        rider.setLevel(level);
        if (trainerId != null) {
            Trainer trainer = new Trainer();
            trainer.setId(trainerId);
            rider.setTrainer(trainer);
        }
        return rider;
    }
}
