package ma.ensias.adminacademic.identity.web.dto;

public record TokenResponse(String accessToken, String refreshToken, String tokenType, long expiresInSeconds) {
}