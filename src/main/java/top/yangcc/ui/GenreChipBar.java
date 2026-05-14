package top.yangcc.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Responsive genre filter bar using FlowPane so chips wrap to multiple rows on
 * small windows and expand to a single row on larger ones.
 */
public class GenreChipBar extends VBox {

    private static final Map<String, Integer> GENRES = new LinkedHashMap<>();
    static {
        GENRES.put("新闻", 1356);
        GENRES.put("社会与文化", 1381);
        GENRES.put("科技", 1403);
        GENRES.put("商业", 1310);
        GENRES.put("教育", 1320);
        GENRES.put("艺术", 1301);
        GENRES.put("娱乐", 1405);
        GENRES.put("音乐", 1352);
        GENRES.put("健康", 1331);
        GENRES.put("历史", 1330);
        GENRES.put("科学", 1371);
        GENRES.put("体育", 1387);
        GENRES.put("喜剧", 1317);
        GENRES.put("犯罪纪实", 1404);
        GENRES.put("儿童与家庭", 1338);
        GENRES.put("宗教与灵性", 1364);
        GENRES.put("政府", 1329);
        GENRES.put("休闲", 1343);
        GENRES.put("小说", 1325);
        GENRES.put("哲学", 1384);
    }

    private final List<Button> chips = new ArrayList<>();
    private BiConsumer<Integer, String> onGenreSelected;

    public GenreChipBar() {
        setPadding(new Insets(0));
        getStyleClass().add("discover-genre-bar");

        FlowPane chipPane = new FlowPane();
        chipPane.setHgap(8);
        chipPane.setVgap(6);
        chipPane.setAlignment(Pos.TOP_LEFT);
        chipPane.setPadding(new Insets(4, 0, 4, 0));
        chipPane.getStyleClass().add("discover-genre-flow");

        boolean first = true;
        for (var entry : GENRES.entrySet()) {
            Button chip = createChip(entry.getKey(), entry.getValue(), entry.getKey());
            if (first) {
                chip.getStyleClass().add("discover-genre-chip-active");
                first = false;
            }
            chips.add(chip);
            chipPane.getChildren().add(chip);
        }

        getChildren().add(chipPane);
    }

    private Button createChip(String label, int genreId, String genreName) {
        Button chip = new Button(label);
        chip.getStyleClass().add("discover-genre-chip");
        chip.setOnAction(e -> {
            selectChip(chip);
            if (onGenreSelected != null) onGenreSelected.accept(genreId, genreName);
        });
        return chip;
    }

    private void selectChip(Button selected) {
        for (Button chip : chips) {
            chip.getStyleClass().remove("discover-genre-chip-active");
        }
        selected.getStyleClass().add("discover-genre-chip-active");
    }

    public void setOnGenreSelected(BiConsumer<Integer, String> handler) { this.onGenreSelected = handler; }
}
