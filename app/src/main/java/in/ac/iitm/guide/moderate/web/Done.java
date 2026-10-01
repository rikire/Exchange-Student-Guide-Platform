package in.ac.iitm.guide.moderate.web;

/**
 * What the queue says after a decision redirects to it (fix 2.3, F-24): "Published:", "Rejected" or
 * "Removed", the title, and a link when there is a page to go to. Carried as a flash attribute, so it
 * shows once.
 *
 * @param path the published article's address, or {@code null} when nothing is left to open
 */
record Done(String what, String title, String path) {

    static final String ATTRIBUTE = "done";
}
