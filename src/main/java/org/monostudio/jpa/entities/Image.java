package org.monostudio.jpa.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.monostudio.jpa.DBEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "images")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class Image
    implements DBEntity {
    private static final long serialVersionUID = 5L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id", nullable = false)
    private Long id;
    @Size(min = 1, max = 50)
    @Column(name = "image_code", nullable = false, unique = true)
    private String code;
    @Size(min = 1, max = 100)
    @Column(name = "image_filename", nullable = false, unique = true)
    private String filename;
    @Size(min = 1, max = 500)
    @Column(name = "image_url", nullable = false, unique = true)
    private String url;

    public Image(Image source) {
        this.id = source.id;
        this.code = source.code;
        this.filename = source.filename;
        this.url = source.url;
    }
}
