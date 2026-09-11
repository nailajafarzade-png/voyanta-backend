package com.voyanta.user.dto.request;

import jakarta.validation.constraints.NotBlank;

// Email bura salınmayıb — email dəyişmək təsdiq (verification) tələb edən
// ayrıca axındır, sadə profil yeniləməsi ilə qarışdırılmamalıdır.
public record UpdateProfileRequest(
        @NotBlank(message = "Ad Soyad boş ola bilməz")
        String fullName,

        String phone
) {}