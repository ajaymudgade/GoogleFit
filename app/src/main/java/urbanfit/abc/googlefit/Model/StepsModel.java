package urbanfit.abc.googlefit.Model;

public class StepsModel {

    public String startDate;
    public String endDate;
    public String stepsCount;
    public String unitSteps;


    public StepsModel(String startDate, String endDate, String stepsCount, String unitSteps) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.stepsCount = stepsCount;
        this.unitSteps = unitSteps;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getStepsCount() {
        return stepsCount;
    }

    public void setStepsCount(String stepsCount) {
        this.stepsCount = stepsCount;
    }

    public String getUnitSteps() {
        return unitSteps;
    }

    public void setUnitSteps(String unitSteps) {
        this.unitSteps = unitSteps;
    }
}
