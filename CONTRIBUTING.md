<p align="center">
<img src="./docs/cdpg.png" width="300">
</p>

We follow Git Merge based workflow
1. Fork this repo
2. Create a new feature branch in your fork. Multiple features must have a hyphen separated name
3. Commit to your fork and raise a Pull Request with upstream

## Code Style
- To maintain a consistent code style maven checkstyle plugin is used : [reference link](https://maven.apache.org/plugins/maven-checkstyle-plugin/index.html)
- To inspect, analyze the code to remove common programming defects,
  inculcate programming best practices PMD is being used : [reference link](https://maven.apache.org/plugins/maven-pmd-plugin/)
- The following maven command is used to generate PMD, checkstyle and Copy/Paste Detector (CPD) reports in `./target/site` folder
  <br> ```mvn checkstyle:checkstyle pmd:pmd pmd:cpd```
- To resolve checkstyle issues faster please install any of the following plugins in the IDE like IntelliJ
  - google-java-format : [link](https://github.com/google/google-java-format?tab=readme-ov-file)
  - CheckStyle-IDEA : [link](https://plugins.jetbrains.com/plugin/1065-checkstyle-idea)

## About commit message
Git commit command is used to save the changes done in the local repository after staging in git.
To add the files in staging
```
git add <file>
```
To commit the file
```
git commit -m "<subject>" -m "<description>"
```

### Commit subject
- The commit subject could be no more than 50 characters
- To maintain consistent commit messages, the subject could be in imperative tone or as a
  verb. Example : `Change query` rather than `Changes made in the query`, `Changed the query`
  <br> This could also reduce the size of subject
- Could contain one of [feat, fix, style, refactor, test, docs, chore]

### Commit description
- Could be detailed to explain the changes made but no more than 72 characters
- Punctuation could be omitted at the end of commit description
- Could also reference an issue or pull request