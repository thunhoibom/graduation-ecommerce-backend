package org.monostudio.api.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(NON_NULL)
public class ImagePojo {
    private Long id;
    @NotBlank
    private String code;
    @NotBlank
    private String filename;
    @NotBlank
    private String url;
    private String altText;
    private String mimeType;
    private Integer width;
    private Integer height;
    private Long fileSize;
}
