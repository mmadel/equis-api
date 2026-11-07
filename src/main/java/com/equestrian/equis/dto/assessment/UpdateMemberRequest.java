package com.equestrian.equis.dto.assessment;

import com.equestrian.equis.common.phone.PhoneNormalizer;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Locale;

// W2's Edit: only the fields that change; null means keep. At least one is checked in MemberDetailsService.
// The constructor cleans the input before validation runs.
public record UpdateMemberRequest(@Size(min = 1, max = 100) String name,
                                  @Pattern(regexp = PhoneNormalizer.LOCAL_FORMAT) String phone,
                                  @Email @Size(min = 1, max = 254) String email) {
    public UpdateMemberRequest {
        name = name == null ? null : name.trim();
        phone = PhoneNormalizer.cleanForValidation(phone);
        email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    public boolean isEmpty() {
        return name == null && phone == null && email == null;
    }
}
