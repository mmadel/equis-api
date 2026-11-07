package com.equestrian.equis.service.platform;

import com.equestrian.equis.common.error.ApiException;
import com.equestrian.equis.common.error.ErrorCodes;
import com.equestrian.equis.common.security.Role;
import com.equestrian.equis.common.tenant.TenantTransactions;
import com.equestrian.equis.dto.accounts.AuthTokens;
import com.equestrian.equis.dto.platform.PlatformLoginRequest;
import com.equestrian.equis.entity.platform.SuperAdmin;
import com.equestrian.equis.repository.platform.SuperAdminRepository;
import com.equestrian.equis.service.accounts.AdminLockoutService;
import com.equestrian.equis.service.accounts.AuthTokenService;
import com.equestrian.equis.service.accounts.PasswordService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;

// The super admin's sign-in. Looks only in super_admin, so club accounts never sign in here (and AuthService never
// looks in super_admin). The lockout is the club admin's: 5 wrong tries lock the account for 15 minutes.
@Service
public class PlatformAuthService {
    private final SuperAdminRepository superAdmins;
    private final AdminLockoutService lockout;
    private final PasswordService passwords;
    private final AuthTokenService tokens;
    private final TenantTransactions tenants;

    public PlatformAuthService(SuperAdminRepository superAdmins, AdminLockoutService lockout,
                               PasswordService passwords, AuthTokenService tokens, TenantTransactions tenants) {
        this.superAdmins = superAdmins;
        this.lockout = lockout;
        this.passwords = passwords;
        this.tokens = tokens;
        this.tenants = tenants;
    }

    // One transaction in root mode. Writes are kept on an ApiException, so a wrong try still counts.
    public AuthTokens login(PlatformLoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        return tenants.asRootKeepingWritesOnApiError(() -> {
            // The row lock stops two wrong tries at the same moment from reading the same counter.
            Optional<SuperAdmin> found = superAdmins.findByEmailForUpdate(email);
            if (found.isEmpty()) {
                // Still runs BCrypt, so an unknown email takes as long as a wrong password.
                passwords.matches(request.password(), null);
                throw new ApiException(HttpStatus.UNAUTHORIZED, ErrorCodes.INVALID_CREDENTIALS,
                        "Wrong email or password.");
            }
            SuperAdmin superAdmin = found.get();
            // Clears the counter and records the login on success; the entity is saved at commit.
            lockout.verifyPassword(superAdmin, request.password());
            return tokens.issue(Role.SUPER_ADMIN, superAdmin.getId(), null, request.rememberMe());
        });
    }
}
