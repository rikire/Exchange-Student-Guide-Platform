package in.ac.iitm.guide.contribute.internal;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * An edit aimed at an address with no published article, and not one the form was opened for and
 * that was removed since ({@link ArticleRemovedWhileEditingException} answers that with {@code 409}).
 * It answers {@code 404}, like the read route.
 */
// trace:FR-011
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ArticleNotPublishedException extends RuntimeException {

    public ArticleNotPublishedException(String address) {
        super("No published article to edit at address: " + address);
    }
}
