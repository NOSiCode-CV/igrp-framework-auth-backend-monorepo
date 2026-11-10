package cv.igrp.sdktest.harness;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.envers.Audited;

/** An audited entity of the test application, so the harness can check Envers revisions (v3-24). */
@Entity
@Audited
public class Note {

    @Id
    @GeneratedValue
    private Long id;

    private String text;

    protected Note() {
    }

    public Note(String text) {
        this.text = text;
    }

    public Long getId() {
        return id;
    }

    public String getText() {
        return text;
    }
}
