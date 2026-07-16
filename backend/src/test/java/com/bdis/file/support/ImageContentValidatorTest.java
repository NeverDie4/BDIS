package com.bdis.file.support;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bdis.common.exception.BusinessException;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ImageContentValidatorTest {

    private final ImageContentValidator validator = new ImageContentValidator();

    @TempDir Path tempDir;

    @Test
    void acceptsPngSignatureWithPngExtension() throws Exception {
        Path image = tempDir.resolve("cover.png");
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "PNG", image.toFile());

        validator.requireAllowedImage(image, "cover.png");
    }

    @Test
    void rejectsNonImageEvenWhenNamedPng() throws Exception {
        Path fake = tempDir.resolve("fake.png");
        Files.writeString(fake, "%PDF-1.7 fake image");

        assertThatThrownBy(() -> validator.requireAllowedImage(fake, "fake.png"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void rejectsSignatureAndExtensionMismatch() throws Exception {
        Path image = tempDir.resolve("cover.png");
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "PNG", image.toFile());

        assertThatThrownBy(() -> validator.requireAllowedImage(image, "cover.jpg"))
                .isInstanceOf(BusinessException.class);
    }
}
