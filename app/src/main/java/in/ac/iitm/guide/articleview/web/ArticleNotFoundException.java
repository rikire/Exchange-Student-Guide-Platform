package in.ac.iitm.guide.articleview.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * The address names no published article: never existed, removed, or only ever a submission that
 * was not approved. All of them look the same from outside, on purpose (ui-routes.md, "Response
 * codes": {@code 404} for a target that must not be revealed to exist).
 */
// trace:FR-001
@ResponseStatus(HttpStatus.NOT_FOUND)
class ArticleNotFoundException extends RuntimeException {

    ArticleNotFoundException(String address) {
        super("No published article at address: " + address);
    }
}
