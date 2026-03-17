package com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction;

import com.sun.jdi.ObjectReference;

/**
 * Ссылка на существующий UniversalElementRepresentation
 */
public class ElementReference extends AbstractElementRepresentation {
    private final AbstractElementRepresentation.Tag referenceTag;

    public ElementReference(AbstractElementRepresentation.Tag ownTag,
                            AbstractElementRepresentation.Tag referenceTag,
                            ObjectReference objectReference) {
        super(ownTag, objectReference);
        this.referenceTag = referenceTag;
    }

    public AbstractElementRepresentation.Tag getReferenceTag() {
        return referenceTag;
    }

    @Override
    public String getElementName() {
        return "<reference to " + referenceTag.getUniqueId() + ">";
    }

	@Override
	public String toString() {
		return "ElementReference [referenceTag=" + referenceTag + ", tag=" + tag + ", getReferenceTag()="
				+ getReferenceTag() + ", getElementName()=" + getElementName() + ", getTag()=" + getTag()
				+ ", getObjectReference()=" + getObjectReference() + ", getClass()=" + getClass() + ", hashCode()="
				+ hashCode() + ", toString()=" + super.toString() + "]";
	}

    
}