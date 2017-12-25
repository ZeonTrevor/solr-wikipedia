package org.solr.wikipedia.model;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Main class the represents a Page from the WikiMedia XML.
 *
 * @author bryanbende
 */
public class Page {

    private final String title;
    
    private final String id;
    
    private final boolean redirect;
    
    private final List<Revision> revisions;

    private Page(PageBuilder builder) {
        this.title = builder.title;
        this.id = builder.id;
        this.redirect = builder.redirect;
        this.revisions = builder.revisions;
        Validate.notEmpty(title);
        Validate.notEmpty(revisions);
    }

    public String getTitle() {
        return title;
    }

    public String getId() {
    	return id;
    }
    
    public boolean isRedirect() {
    	return redirect;
    }
    
    public List<Revision> getRevisions() {
        return this.revisions;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).
                append(title).
                append(id).
                append(redirect).
                append(revisions).
                toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) { return false; }
        if (obj == this) { return true; }
        if (obj.getClass() != getClass()) {
            return false;
        }

        Page that = (Page) obj;
        return new EqualsBuilder()
                .append(this.title, that.title)
                .append(this.id, that.id)
                .append(this.redirect, that.redirect)
                .append(this.revisions, that.revisions)
                .isEquals();
    }

    @Override
    public String toString() {
        return "Page [title = " + title + ", id = " + id + ", redirect = " + redirect + "]";
    }

    /**
     * A builder for Page instances.
     */
    public static class PageBuilder {
        private String title;
        private String id;
        private boolean redirect;
        private List<Revision> revisions;

       public PageBuilder title(String title) {
            this.title = title;
            return this;
        }
       
       public PageBuilder id(String id) {
    	   this.id = id;
    	   return this;
       }

       public PageBuilder redirect(boolean redirect) {
    	   this.redirect = redirect;
    	   return this;
       }
       
        public PageBuilder revision(Revision revision) {
            if (this.revisions == null) {
                this.revisions = new ArrayList<>();
            }
            this.revisions.add(revision);
            return this;
        }

        public Page build() {
            return new Page(this);
        }
    }

}
