package in.ac.iitm.guide.moderate.internal;

/** A decision on a submission that was approved or rejected before: it is never decided twice. */
public class AlreadyDecidedException extends RuntimeException {

    public AlreadyDecidedException(String number) {
        super("Submission " + number + " is no longer pending");
    }
}
