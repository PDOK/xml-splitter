# xml-splitter

Splits a xml-file (source) in more small xml-files, the file will be divided on the element 'splitter'.
Each file will have the header and footer of the original file (source).
The size of the small xml-files will be based on max-elements in one file.
The parameter target will be used to name the new xml-files.

# example
java -jar xml-splitter-0.1.0-SNAPSHOT-standalone.jar D:/temp/BebouwingFirst1000Lines.gml building gbkn:FeatureMember 2
