package com.PruebaSimed2.utils;

import javafx.scene.paint.Color;
import java.util.*;

public class ColorGenerator {

    // Paleta extensa de colores profesionales y distinguibles
    private static final String[] COLOR_PALETTE = {
            "#3498db", "#e74c3c", "#2ecc71", "#f39c12", "#9b59b6",
            "#1abc9c", "#e67e22", "#34495e", "#d35400", "#16a085",
            "#27ae60", "#2980b9", "#8e44ad", "#2c3e50", "#f1c40f",
            "#e74c3c", "#ecf0f1", "#95a5a6", "#7f8c8d", "#bdc3c7",
            "#FF6B6B", "#4ECDC4", "#45B7D1", "#96CEB4", "#FFEAA7",
            "#DDA0DD", "#98D8C8", "#F7DC6F", "#BB8FCE", "#85C1E9"
    };

    private static final Map<String, Color> medicoColors = new HashMap<>();
    private static int currentIndex = 0;

    public static Color getColorForMedico(String medicoNombre) {
        if (medicoColors.containsKey(medicoNombre)) {
            return medicoColors.get(medicoNombre);
        }

        // Asignar nuevo color
        Color newColor = Color.web(COLOR_PALETTE[currentIndex % COLOR_PALETTE.length]);
        medicoColors.put(medicoNombre, newColor);
        currentIndex++;

        return newColor;
    }

    public static void resetColors() {
        medicoColors.clear();
        currentIndex = 0;
    }

    // Método para obtener colores en formato CSS
    public static String getColorStyleForMedico(String medicoNombre) {
        Color color = getColorForMedico(medicoNombre);
        return String.format("-fx-bar-fill: %s; -fx-background-color: %s;",
                toHex(color), toHex(color));
    }

    private static String toHex(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }
}
