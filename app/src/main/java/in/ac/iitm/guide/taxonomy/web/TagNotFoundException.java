package in.ac.iitm.guide.taxonomy.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * No published article carries a tag with this address: no such tag, or one only a pending, rejected
 * or removed article carries. They look the same from outside, so the page does not reveal what the
 * queue holds (FEAT-008).
 */
// trace:FR-008
@ResponseStatus(HttpStatus.NOT_FOUND)
class TagNotFoundException extends RuntimeException {

    TagNotFoundException(String address) {
        super("No published article carries a tag at address: " + address);
    }
}
