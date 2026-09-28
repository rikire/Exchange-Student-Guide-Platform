package in.ac.iitm.guide.media;

import org.springframework.core.io.InputStreamSource;

/**
 * A file as a contributor sent it. {@code originalName} and the declared type are chosen by a
 * stranger: the name is kept for display only, and the type is read from {@code content}.
 *
 * @param originalName the file's name on the contributor's device
 * @param size its length in bytes, as the container received it
 * @param content the bytes, which may be read more than once
 */
// trace:FR-010
// trace:FR-011
public record Upload(String originalName, long size, InputStreamSource content) {}
