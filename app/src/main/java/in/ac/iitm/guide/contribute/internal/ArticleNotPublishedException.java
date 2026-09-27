package in.ac.iitm.guide.contribute.internal;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * An edit aimed at an address with no published article: never there, or removed. FR-011's "no
 * longer published" is unconditional, so both answer {@code 404}, like the read route.
 */
// TODO(DEBT-009): answer 409 for an article removed while the form was open
// trace:FR-011
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ArticleNotPublishedException extends RuntimeException {

    ArticleNotPublishedException(String address) {
        super("No published article to edit at address: " + address);
    }
}
