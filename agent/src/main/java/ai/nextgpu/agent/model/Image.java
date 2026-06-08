package ai.nextgpu.agent.model;

import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Image {
    BufferedImage image;
    String filename;
    int sizeInBytes;
    boolean isGrayscale;

    public Image(String filepath) throws IOException {
        image = ImageIO.read(new File(filepath));
        this.filename = filename;
        this.sizeInBytes = image.getWidth() * image.getHeight() * (image.getColorModel().getPixelSize() / 8);
        this.isGrayscale = image.getColorModel().getNumComponents() == 1;
    }

    public Image(BufferedImage image, String filename) {
        this.image = image;
        this.filename = filename;
        this.sizeInBytes = image.getWidth() * image.getHeight() * (image.getColorModel().getPixelSize() / 8);
        this.isGrayscale = image.getColorModel().getNumComponents() == 1;
    }

    public int getWidth() {
        return image.getWidth();
    }

    public int getHeight() {
        return image.getHeight();
    }
}
