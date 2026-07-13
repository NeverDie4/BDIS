package com.bdis.file.support;

import com.bdis.common.enums.ResultCodeEnum;
import com.bdis.common.exception.BusinessException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ImageContentValidator {

    private static final Map<String, Set<String>> ALLOWED_FORMATS =
            Map.of(
                    "JPEG", Set.of("jpg", "jpeg"),
                    "PNG", Set.of("png"),
                    "GIF", Set.of("gif"),
                    "BMP", Set.of("bmp"));

    public void requireAllowedImage(Path path, String filename) {
        String extension = StringUtils.getFilenameExtension(filename);
        if (!StringUtils.hasText(extension)) {
            throw invalidImage();
        }

        try (ImageInputStream input = ImageIO.createImageInputStream(path.toFile())) {
            if (input == null) {
                throw invalidImage();
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw invalidImage();
            }
            ImageReader reader = readers.next();
            try {
                String format = reader.getFormatName().toUpperCase(Locale.ROOT);
                Set<String> extensions = ALLOWED_FORMATS.get(format);
                if (extensions == null
                        || !extensions.contains(extension.toLowerCase(Locale.ROOT))) {
                    throw invalidImage();
                }
            } finally {
                reader.dispose();
            }
        } catch (IOException exception) {
            throw new BusinessException(ResultCodeEnum.VALIDATION_ERROR, "无法验证地图封面图片内容");
        }
    }

    private BusinessException invalidImage() {
        return new BusinessException(
                ResultCodeEnum.VALIDATION_ERROR, "地图封面仅支持内容与扩展名一致的 JPG、JPEG、PNG、GIF 或 BMP 图片");
    }
}
