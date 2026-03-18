package com.gmail.aydinov.sergey.simple_debugger_plugin.abstraction;

import java.util.Objects;
import java.util.UUID;
import com.sun.jdi.ObjectReference;

public abstract class AbstractElementRepresentation {
    protected final Tag tag;
    private final ObjectReference objectReference;

    protected AbstractElementRepresentation(Tag tag, ObjectReference objectReference) {
        this.tag = tag;
        this.objectReference = objectReference;
    }

    public Tag getTag() {
        return tag;
    }

    public ObjectReference getObjectReference() {
        return objectReference;
    }
    
    

    @Override
	public String toString() {
		return "AbstractElementRepresentation [tag=" + tag + ", objectReference=" + objectReference + "]";
	}

	public abstract String getElementName();

    // =================== Вложенный статический класс Tag ===================
    public static final class Tag {
        private final UUID uniqueId;
        private final UUID parentId;

        public Tag(UUID uniqueId, UUID parentId) {
            this.uniqueId = uniqueId;
            this.parentId = parentId;
        }

        public UUID getUniqueId() { return uniqueId; }
        public UUID getParentId() { return parentId; }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Tag other = (Tag) obj;
            return Objects.equals(uniqueId, other.uniqueId) &&
                   Objects.equals(parentId, other.parentId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(uniqueId, parentId);
        }

        @Override
        public String toString() {
            return "Tag{uniqueId=" + uniqueId + ", parentId=" + parentId + "}";
        }
    }
}