package in.ac.iitm.guide.backup;

/**
 * A file of an archive cannot become an article. The message names the file and what is wrong with
 * it; the import that raised it has changed nothing.
 */
// trace:NFR-004
public class ArchiveFormatException extends RuntimeException {

    public ArchiveFormatException(String message) {
        super(message);
    }
}
