package tn.iteam.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "users")
public class User extends BaseEntity {

    private String username;
    /**
     * @deprecated Passwords are managed by Keycloak. This field is retained for
     * backward compatibility only and must not be used for authentication.
     */
    @Deprecated(since = "keycloak-integration")
    private String password;
    private String email;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    private Role role;
}
