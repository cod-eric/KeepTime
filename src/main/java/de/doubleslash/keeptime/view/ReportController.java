package de.doubleslash.keeptime.view;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.javafx.scene.control.skin.DatePickerSkin;

import de.doubleslash.keeptime.common.DateFormatter;
import de.doubleslash.keeptime.model.Model;
import de.doubleslash.keeptime.model.Project;
import de.doubleslash.keeptime.model.Work;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Callback;

public class ReportController {

   private static final String FX_BACKGROUND_COLOR_NOT_WORKED = "-fx-background-color: #BBBBBB;";

   @FXML
   private BorderPane topBorderPane;

   @FXML
   private Label currentDayLabel;
   @FXML
   private Label currentDayWorkTimeLabel;
   @FXML
   private Label currentDayTimeLabel;

   @FXML
   private GridPane gridPane;
   @FXML
   private ScrollPane scrollPane;

   private static final Logger LOG = LoggerFactory.getLogger(ReportController.class);

   private DatePicker datePicker; // for calender element

   private Model model;

   @FXML
   private void initialize() {
      LOG.info("Init reportController");

      datePicker = new DatePicker(LocalDate.now());
      datePicker.valueProperty().addListener((observable, oldvalue, newvalue) -> {
         LOG.info("Datepicker selected value changed to {}", newvalue);
         updateReport(newvalue);
      });
   }

   private void updateReport(final LocalDate newvalue) {
      currentDayLabel.setText(DateFormatter.toDayDateString(newvalue));
      final List<Work> currentWorkItems = model.getWorkRepository().findByCreationDate(newvalue);

      final SortedSet<Project> workedProjectsSet = currentWorkItems.stream().map(Work::getProject)
            .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(Project::getName))));

      gridPane.getChildren().clear();
      gridPane.getRowConstraints().clear();

      int rowIndex = 0;
      long currentWorkSeconds = 0;
      long currentSeconds = 0;
      final Font labelFontBold = Font.font("System", FontWeight.BOLD, 15);
      final Font labelFontNormal = Font.font("System", FontWeight.NORMAL, 15);

      for (final Project project : workedProjectsSet) {
         final Label projectName = new Label(project.getName());

         projectName.setFont(labelFontBold);
         gridPane.add(projectName, 0, rowIndex);

         final List<Work> onlyCurrentProjectWork = currentWorkItems.stream().filter(w -> w.getProject() == project)
               .collect(Collectors.toList());

         final long todaysWorkSeconds = onlyCurrentProjectWork.stream()
               .mapToLong(work -> DateFormatter.getSecondsBewtween(work.getStartTime(), work.getEndTime())).sum();

         currentSeconds += todaysWorkSeconds;
         if (project.isWork()) {
            currentWorkSeconds += todaysWorkSeconds;
         }

         final Label workedTimeLabel = new Label(DateFormatter.secondsToHHMMSS(todaysWorkSeconds));
         workedTimeLabel.setFont(labelFontBold);

         gridPane.add(workedTimeLabel, 2, rowIndex);
         rowIndex++;

         for (int j = 0; j < onlyCurrentProjectWork.size(); j++) {
            final Work work = onlyCurrentProjectWork.get(j);
            final String workedHours = DateFormatter
                  .secondsToHHMMSS(DateFormatter.getSecondsBewtween(work.getStartTime(), work.getEndTime()));

            final Label commentLabel = new Label(work.getNotes());
            commentLabel.setFont(labelFontNormal);
            commentLabel.setWrapText(true);
            gridPane.add(commentLabel, 0, rowIndex);

            final Label fromTillLabel = new Label(DateFormatter.toTimeString(work.getStartTime()) + " - "
                  + DateFormatter.toTimeString(work.getEndTime()));
            fromTillLabel.setFont(labelFontNormal);
            fromTillLabel.setWrapText(true);
            gridPane.add(fromTillLabel, 1, rowIndex);

            final Label workedHoursLabel = new Label(workedHours);
            workedHoursLabel.setFont(labelFontNormal);
            gridPane.add(workedHoursLabel, 2, rowIndex);

            rowIndex++;
         }
      }
      scrollPane.setVvalue(0); // scroll to the top

      currentDayTimeLabel.setText(DateFormatter.secondsToHHMMSS(currentSeconds));
      currentDayWorkTimeLabel.setText(DateFormatter.secondsToHHMMSS(currentWorkSeconds));
   }

   public void setModel(final Model model) {
      this.model = model;

      // HACK to show calendar from datepicker
      // https://stackoverflow.com/questions/34681975/javafx-extract-calendar-popup-from-datepicker-only-show-popup
      final DatePickerSkin datePickerSkin = new DatePickerSkin(datePicker);
      final Callback<DatePicker, DateCell> dayCellFactory = callback -> new DateCell() {
         @Override
         public void updateItem(final LocalDate item, final boolean empty) {
            super.updateItem(item, empty);
            if (model.getWorkRepository().findByCreationDate(item).isEmpty()) {
               setDisable(true);
               setStyle(FX_BACKGROUND_COLOR_NOT_WORKED);
            }
         }
      };

      datePicker.setDayCellFactory(dayCellFactory);
      final Node popupContent = datePickerSkin.getPopupContent();
      topBorderPane.setRight(popupContent);
   }

   public void update() {
      updateReport(datePicker.getValue());
   }
}
