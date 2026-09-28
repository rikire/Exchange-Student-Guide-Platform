package in.ac.iitm.guide.contribute.internal;

/**
 * The article an edit form was opened for was removed (FR-026) before the edit was sent. It answers
 * {@code 409}, "this changed while you were editing", where an address that never had an article
 * answers {@code 404} (DEBT-009, closed).
 */
// trace:FR-011
public class ArticleRemovedWhileEditingException extends RuntimeException {

    ArticleRemovedWhileEditingException() {
        super("The article was removed while the edit was being written");
    }
}
