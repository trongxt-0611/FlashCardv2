package team.loser.kanjiflashcard.models;

public class Set {
    private  String id, name, description, timeStamp;

    public Set() {
    }

    public Set(String id, String name, String description, String timeStamp) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.timeStamp = timeStamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }
}
