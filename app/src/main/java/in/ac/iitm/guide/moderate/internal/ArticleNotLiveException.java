package in.ac.iitm.guide.moderate.internal;

/** An address with no published article: never there, or removed already. */
public class ArticleNotLiveException extends RuntimeException {

    public ArticleNotLiveException(String address) {
        super("No published article at address: " + address);
    }
}
