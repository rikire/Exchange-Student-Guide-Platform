package in.ac.iitm.guide.moderate.internal;

/** A number that was never issued. */
public class SubmissionNotFoundException extends RuntimeException {

    public SubmissionNotFoundException(String number) {
        super("No submission " + number);
    }
}
