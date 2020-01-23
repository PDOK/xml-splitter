# xml-splitter

Splits a xml-file (source) in smaller xml-files, the file will be divided on the element 'splitted-on', or the autodetected element if `--auto` is defined. 
Each file will have the header and footer of the original file (source).
The size of the small xml-files will be based on max-elements in one file.

If auto-mode is defined all files will be transformed in one run. In non auto-mode you can transform one file at a time.

See `--help` for more info.

# example
`java -jar xml-splitter.jar --zip --auto path/to/file1.gml path/to/file2.gml`

## Docker build
`docker build -t xml-splitter .`

## Docker run example
`docker run -it --rm --name xml-splitter -v /folder/xml:/usr/src/app/xml xml-splitter java -jar xml-splitter.jar -v /folder/splitted:/usr/src/app/splitted --auto xml/bigfile.xml`

