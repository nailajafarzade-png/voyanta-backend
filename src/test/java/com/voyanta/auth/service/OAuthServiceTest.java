package com.voyanta.auth.service;

import com.voyanta.auth.dao.entity.User;
import com.voyanta.auth.dao.repository.UserRepository;
import com.voyanta.auth.dto.request.OAuthLoginRequest;
import com.voyanta.auth.dto.response.AuthResponse;
import com.voyanta.auth.dto.response.UserSummary;
import com.voyanta.auth.enums.AuthProvider;
import com.voyanta.auth.oauth.ProviderTokenVerifier;
import com.voyanta.auth.oauth.VerifiedOidcUser;
import com.voyanta.common.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuthServiceTest {

    @Mock
    private ProviderTokenVerifier googleVerifier;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuthService authService;

    private OAuthService oAuthService;

    @BeforeEach
    void setUp() {
        oAuthService = new OAuthService(Map.of("google", googleVerifier), userRepository, authService);
    }

    @Test
    void createsUserOnFirstLogin() {
        when(googleVerifier.verify("token")).thenReturn(new VerifiedOidcUser("sub", "new@voyanta.test", "Ada"));
        when(userRepository.findByEmail("new@voyanta.test")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });
        AuthResponse issued = new AuthResponse("a", "r", new UserSummary(UUID.randomUUID(), "Ada", "new@voyanta.test"));
        when(authService.issueTokens(any(User.class))).thenReturn(issued);

        AuthResponse response = oAuthService.login("google", new OAuthLoginRequest("token"));
        assertThat(response).isSameAs(issued);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("new@voyanta.test");
        assertThat(captor.getValue().getProvider()).isEqualTo(AuthProvider.GOOGLE);
    }

    @Test
    void unknownProviderIsRejected() {
        assertThatThrownBy(() -> oAuthService.login("facebook", new OAuthLoginRequest("token")))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getErrorCode())
                .isEqualTo("UNKNOWN_PROVIDER");
    }
}
