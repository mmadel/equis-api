package com.equestrian.equis.support;

import com.equestrian.equis.support.AdminOversightFixture.Coach;
import com.jayway.jsonpath.JsonPath;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.equestrian.equis.support.AdminOversightFixture.read;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// The payment side of the scenario tests (E12, E13), over HTTP: the admin records and voids payments and reads W3
// and a rider's history; the trainers read My riders. assertPaymentInvariants() checks the payment rules that span
// those screens. Built on AdminOversightFixture, for the trainers the scenario uses.
public class PaymentJourneys {
    private final AdminOversightFixture fx;
    private final List<Coach> coaches;

    public PaymentJourneys(AdminOversightFixture fx, Coach... coaches) {
        this.fx = fx;
        this.coaches = List.of(coaches);
    }

    // ---- The admin on W3

    // POST /admin/riders/{id}/payments, whatever the answer; the default 8 sessions per month.
    public ResultActions payRequest(UUID riderId, String amount, YearMonth start, int months) throws Exception {
        return payRequest(riderId, amount, start, months, null);
    }

    // The same with an explicit number of sessions; null leaves it out.
    public ResultActions payRequest(UUID riderId, String amount, YearMonth start, int months, Integer sessions)
            throws Exception {
        String extra = sessions == null ? "" : ", \"sessions\": " + sessions;
        return fx.accounts().admin(post("/admin/riders/" + riderId + "/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"amountEgp\": %s, \"method\": \"cash\", \"startMonth\": \"%s\", \"months\": %d%s }"
                        .formatted(amount, start, months, extra)));
    }

    public ResultActions voidRequest(UUID paymentId, String reason) throws Exception {
        return fx.accounts().admin(post("/admin/payments/" + paymentId + "/void")
                .contentType(MediaType.APPLICATION_JSON).content("{ \"reason\": \"%s\" }".formatted(reason)));
    }

    public ResultActions history(UUID riderId) throws Exception {
        return fx.accounts().admin(get("/admin/riders/" + riderId + "/payments")).andExpect(status().isOk());
    }

    // ---- What the screens show

    public void assertBalance(UUID riderId, int sessionsLeft, String status, String nextExpiry) throws Exception {
        Map<String, Object> expected = new HashMap<>();
        expected.put("sessionsLeft", sessionsLeft);
        expected.put("status", status);
        expected.put("nextExpiry", nextExpiry);
        List<Map<String, Object>> w3 = JsonPath.read(body(fx.w3()),
                AdminOversightFixture.riderRow(riderId) + ".balance");
        assertThat(w3).as("W3 balance").singleElement().isEqualTo(expected);
    }

    // The trainer's My riders: each rider id with its balance.
    public Map<String, Map<String, Object>> onMyRiders(Coach coach) throws Exception {
        List<Map<String, Object>> rows = JsonPath.read(body(fx.myRiders(coach)), "$");
        Map<String, Map<String, Object>> byId = new HashMap<>();
        for (Map<String, Object> row : rows) {
            @SuppressWarnings("unchecked")
            Map<String, Object> balance = (Map<String, Object>) row.get("balance");
            byId.put((String) row.get("id"), balance);
        }
        return byId;
    }

    public long w3Total() throws Exception {
        return ((Number) JsonPath.read(body(fx.w3()), "$.totals.awaitingPayment")).longValue();
    }

    public List<String> w3Ids(Map<String, String> params) throws Exception {
        return new ArrayList<>(JsonPath.read(body(fx.w3(params)), "$.riders[*].id"));
    }

    // ---- The rules that span the endpoints

    // For every rider on W3: the trainer's My riders shows the same balance (and only their trainer lists them);
    // the history agrees; sessionsLeft is the sum of what the active packages have left (upcoming ones don't count
    // yet); the status is active exactly when something is left; nextExpiry is the soonest expiry among the active
    // packages with sessions left. totals.awaitingPayment is the number of awaiting rows.
    public void assertPaymentInvariants() throws Exception {
        String w3 = body(fx.w3());
        List<Map<String, Object>> rows = JsonPath.read(w3, "$.riders");

        Map<String, Map<String, Object>> listed = new HashMap<>();
        Map<String, String> listedBy = new HashMap<>();
        for (Coach coach : coaches) {
            onMyRiders(coach).forEach((riderId, balance) -> {
                listed.put(riderId, balance);
                listedBy.put(riderId, coach.id().toString());
            });
        }

        long awaiting = 0;
        int withTrainer = 0;
        for (Map<String, Object> row : rows) {
            String id = (String) row.get("id");
            @SuppressWarnings("unchecked")
            Map<String, Object> balance = (Map<String, Object>) row.get("balance");
            @SuppressWarnings("unchecked")
            Map<String, Object> trainer = (Map<String, Object>) row.get("trainer");
            if (trainer != null) {
                withTrainer++;
                assertThat(listedBy.get(id)).as("%s is on their trainer's My riders", id).isEqualTo(trainer.get("id"));
                assertThat(listed.get(id)).as("%s: My riders balance = W3 balance", id).isEqualTo(balance);
            }

            String history = body(history(UUID.fromString(id)));
            assertThat(JsonPath.<Map<String, Object>>read(history, "$.balance"))
                    .as("%s: history balance = W3 balance", id).isEqualTo(balance);
            List<Integer> left = JsonPath.read(history, "$.payments[?(@.state == 'active')].left");
            int sessionsLeft = (Integer) balance.get("sessionsLeft");
            assertThat(sessionsLeft).as("%s: sessionsLeft = sum over active packages", id)
                    .isEqualTo(left.stream().mapToInt(Integer::intValue).sum());
            assertThat(balance.get("status")).as("%s: status follows the balance", id)
                    .isEqualTo(sessionsLeft > 0 ? "active" : "awaiting_payment");
            List<String> expiries = JsonPath.read(history,
                    "$.payments[?(@.state == 'active' && @.left > 0)].validUntil");
            assertThat(balance.get("nextExpiry")).as("%s: nextExpiry is the soonest with sessions left", id)
                    .isEqualTo(expiries.stream().sorted().findFirst().orElse(null));
            if (sessionsLeft == 0) {
                awaiting++;
            }
        }
        assertThat(listed).as("My riders lists only riders W3 gives a trainer").hasSize(withTrainer);
        assertThat(((Number) JsonPath.read(w3, "$.totals.awaitingPayment")).longValue())
                .as("awaitingPayment total = awaiting rows").isEqualTo(awaiting);
    }

    // ---- Helpers

    public static UUID paymentId(ResultActions created) throws Exception {
        return UUID.fromString(read(created, "$.payment.id"));
    }

    public static String body(ResultActions result) throws Exception {
        return result.andReturn().getResponse().getContentAsString();
    }
}
