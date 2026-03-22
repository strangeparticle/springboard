# Springboard Licensing Notes

This project includes legal information for three separate concerns:

- Springboard's own source code
- third-party application libraries redistributed with Springboard
- the bundled Java runtime used by desktop distributions

## Springboard and Third-Party Legal Files

Springboard stores legal files in `composeApp/src/commonMain/resources/legal/`:

- `LICENSE.txt`
  - Springboard's own BSD 3-Clause license
- `THIRD_PARTY_NOTICES.txt`
  - Springboard's third-party notices for the non-JDK libraries redistributed with the app

### License Text Duplication
The project license text is intentionally duplicated between the repo-root `LICENSE` file and
`composeApp/src/commonMain/resources/legal/LICENSE.txt`.

Trying to source license text from a single file would have increased build and runtime complexity.

The root file follows standard repository conventions and supports source hosting and license
detection, while the resource copy lets the application bundle and display (in the dialog) the
same license text.

## Builder-Managed Java Runtime Legal Files (Desktop builds)

Compose desktop packaging builds a bundled Java runtime from the JDK used for packaging.

The builder includes the bundled runtime's legal files automatically under:

- `Springboard.app/Contents/runtime/Contents/Home/legal/...`

## In-App License Dialog

A licensing dialog is available within the app that shows:

- the full Springboard license text
- the full Springboard third-party notices text
- a short note explaining that additional bundled Java runtime legal files are included in the application package

The dialog sources its content from the legal resource files noted above.
