package mosaic.ui.actions;

import colors.LEGOColor;
import mosaic.controllers.ColorController;
import mosaic.controllers.StudEditController;
import mosaic.ui.EditTool;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Acción que permite elegir un color LEGO para el pincel sin depender del eyedropper.
 */
public class SelectBrushColorAction extends AbstractAction {
    private final Component parent;
    private final StudEditController studEditController;
    private final ColorController colorController;

    public SelectBrushColorAction(Component parent, StudEditController studEditController, ColorController colorController) {
        super("Manual Color");
        putValue(Action.SHORT_DESCRIPTION, "Pick a brush color even if it is not present in the mosaic");
        this.parent = parent;
        this.studEditController = studEditController;
        this.colorController = colorController;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        List<LEGOColor> availableColors = colorController.getColorsFromDisk();
        if (availableColors == null || availableColors.isEmpty()) {
                JOptionPane.showMessageDialog(parent,
                    "No colors are available for selection.",
                    "Colors not loaded",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<LEGOColor> sortedColors = new ArrayList<>(availableColors);
        sortedColors.sort(Comparator.comparing(LEGOColor::getName, String.CASE_INSENSITIVE_ORDER));

        BrushColorPickerPanel pickerPanel = new BrushColorPickerPanel(sortedColors, studEditController.getSelectedColor());
        int result = JOptionPane.showConfirmDialog(parent,
                pickerPanel,
                "Select brush color",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            LEGOColor selected = pickerPanel.getSelectedColor();
            if (selected != null) {
                studEditController.setSelectedColor(selected);
                studEditController.setActiveTool(EditTool.BRUSH);
            }
        }
    }

    /**
     * Panel simple que lista los colores LEGO con una vista previa para el usuario.
     */
    private static class BrushColorPickerPanel extends JPanel {
        private final JList<LEGOColor> colorList;

        BrushColorPickerPanel(List<LEGOColor> colors, LEGOColor initialSelection) {
            super(new BorderLayout(8, 8));
            JLabel title = new JLabel("Select an available LEGO color:");
            add(title, BorderLayout.NORTH);

            DefaultListModel<LEGOColor> model = new DefaultListModel<>();
            for (LEGOColor color : colors) {
                model.addElement(color);
            }

            colorList = new JList<>(model);
            colorList.setCellRenderer(new LegoColorRenderer());
            colorList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            colorList.setVisibleRowCount(12);
            if (initialSelection != null) {
                colorList.setSelectedValue(initialSelection, true);
            } else if (!colors.isEmpty()) {
                colorList.setSelectedIndex(0);
            }

            add(new JScrollPane(colorList), BorderLayout.CENTER);
        }

        LEGOColor getSelectedColor() {
            return colorList.getSelectedValue();
        }
    }

    private static class LegoColorRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof LEGOColor color) {
                StringBuilder sb = new StringBuilder(color.getName());
                sb.append("  (ID ");
                sb.append(color.getIDRebrickable());
                sb.append(")");
                label.setText(sb.toString());
                label.setIcon(new ColorSwatchIcon(color.getRGB()));
                label.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
            }
            return label;
        }
    }

    /**
     * Icono pequeño que muestra el color LEGO actual en la lista.
     */
    private static class ColorSwatchIcon implements Icon {
        private static final int SIZE = 14;
        private final Color color;

        ColorSwatchIcon(Color color) {
            this.color = color != null ? color : Color.GRAY;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(Color.BLACK);
            g.drawRect(x, y, SIZE, SIZE);
            g.setColor(color);
            g.fillRect(x + 1, y + 1, SIZE - 1, SIZE - 1);
        }

        @Override
        public int getIconWidth() {
            return SIZE + 2;
        }

        @Override
        public int getIconHeight() {
            return SIZE + 2;
        }
    }
}
