package in.ac.iitm.guide.shared.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * A free-form label, stored trimmed and lower-cased by the {@code taxonomy} slice before it reaches
 * this table (ADR-0005) — this entity does not normalise on its own.
 */
// trace:FR-008
@Entity
@Table(name = "tag")
public class Tag {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    /** FR-031: openings of the tag's page, written only by an atomic update (ADR-0017). */
    @Column(name = "visit_count", nullable = false)
    private long visitCount;

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getVisitCount() {
        return visitCount;
    }
}
