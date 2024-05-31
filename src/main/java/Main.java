import com.traneptora.jxlatte.JXLDecoder;
import com.traneptora.jxlatte.JXLImage;
import com.traneptora.jxlatte.io.PNGWriter;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) throws IOException {
        var path = Path.of("jungle.jxl");

        var image = new JXLDecoder(Files.newInputStream(path)).decode();

        showAndSaveImage("Blep", image);
    }

    public static void showAndSaveImage(String title, JXLImage image) {
        // Display the image in a JFrame
        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        ImageIcon imageIcon = new ImageIcon(image.asBufferedImage());
        JLabel jLabel = new JLabel(imageIcon);

        frame.getContentPane().add(jLabel, BorderLayout.CENTER);
        frame.pack();
        frame.setLocationRelativeTo(null); // Center the window
        frame.setVisible(true);

        // Save the image as a PNG file with the lowercase version of the title
        String fileName = title.toLowerCase().replaceAll("\\s+", "_") + ".png";
        File outputFile = new File("C:\\Users\\water\\Downloads\\" + fileName);
        try {
            new PNGWriter(image).write(new FileOutputStream(outputFile));
            System.out.println("Image saved as: " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
