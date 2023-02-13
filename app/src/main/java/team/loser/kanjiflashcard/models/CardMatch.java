package team.loser.kanjiflashcard.models;

public class CardMatch {
    private String id;
    private String text;

    private String hiddenMatchingText;

    public CardMatch(String id, String text, String hiddenMatchingText) {
        this.id = id;
        this.text = text;
        this.hiddenMatchingText = hiddenMatchingText;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getHiddenMatchingText() {
        return hiddenMatchingText;
    }

    public void setHiddenMatchingText(String hiddenMatchingText) {
        this.hiddenMatchingText = hiddenMatchingText;
    }
}
