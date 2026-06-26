//? if >=1.17 {
package com.iamkaf.konfig.impl.v1;

final class KonfigRowFactory {
    private final KonfigRowHost host;

    KonfigRowFactory(KonfigRowHost host) {
        this.host = host;
    }

    KonfigConfigRow create(EntryRef entry) {
        if (entry.value.kind() == EntryKind.HEADER) {
            return new HeaderRow(this.host, entry);
        }
        if (entry.value.kind() == EntryKind.IMAGE) {
            return new ImageRow(this.host, entry);
        }
        if (entry.value.kind() == EntryKind.INLINE_TEXT) {
            return new InlineTextRow(this.host, entry);
        }
        if (entry.value.kind() == EntryKind.URL) {
            return new UrlRow(this.host, entry);
        }
        if (!entry.editable) {
            return new UnsupportedRow(this.host, entry);
        }
        if (entry.value.kind() == EntryKind.BOOLEAN) {
            return new BooleanRow(this.host, entry);
        }
        if (entry.value.kind() == EntryKind.ENUM) {
            return new EnumRow(this.host, entry);
        }
        if (entry.value.kind() == EntryKind.COLOR_RGB || entry.value.kind() == EntryKind.COLOR_ARGB) {
            return new ColorRow(this.host, entry);
        }
        if (entry.value.kind() == EntryKind.STRING_LIST) {
            return new StringListRow(this.host, entry);
        }
        if (entry.value.kind() == EntryKind.DROPDOWN) {
            return new DropdownRow(this.host, entry);
        }
        if (entry.value.kind() == EntryKind.INTEGER && entry.value.hasNumericRange()) {
            return new IntegerSliderRow(this.host, entry);
        }
        if (entry.value.kind() == EntryKind.LONG && entry.value.hasNumericRange()) {
            return new LongSliderRow(this.host, entry);
        }
        if (entry.value.kind() == EntryKind.DOUBLE && entry.value.hasNumericRange()) {
            return new DoubleSliderRow(this.host, entry);
        }
        if (entry.value.kind() == EntryKind.STRING && entry.value.hasBoundRegistry()) {
            return new RegistryTextInputRow(this.host, entry);
        }
        return new TextInputRow(this.host, entry);
    }
}
//?}
