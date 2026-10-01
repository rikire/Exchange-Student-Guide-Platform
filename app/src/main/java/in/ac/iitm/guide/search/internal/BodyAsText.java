package in.ac.iitm.guide.search.internal;

import in.ac.iitm.guide.wikilink.WikiLinkRenderer;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;

/**
 * Indexes an article's body as the words a reader reads (FR-007): the passage a result quotes then
 * holds no Markdown, and a link's address is not searched as though it were text. The two marks the
 * highlighter puts around a matched word are taken out first, so a body cannot fake one.
 */
// trace:FR-007
class BodyAsText implements ValueBridge<String, String> {

    private final WikiLinkRenderer renderer = new WikiLinkRenderer();

    @Override
    public String toIndexedValue(String body, ValueBridgeToIndexedValueContext context) {
        if (body == null) {
            return null;
        }
        return renderer.plainText(body).replace(Passage.OPEN, "").replace(Passage.CLOSE, "");
    }
}
