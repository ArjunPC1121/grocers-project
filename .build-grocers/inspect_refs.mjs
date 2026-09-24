import { FileBlob, PresentationFile } from "@oai/artifact-tool";

const paths = [
  "C:/Users/Arjun P Chandra/Downloads/Oracle Corporate PPT Template.potx",
  "C:/Users/Arjun P Chandra/Desktop/Training/Java/oracle-ofsaadev-21staugust2026/case studies/Case_Study_Presentation_Batch_3/Oracle_Case_Study_Presentation_Team1_Batch_3.pptx",
];
for (const sourcePath of paths) {
  const pres = await PresentationFile.importPptx(await FileBlob.load(sourcePath));
  const report = await pres.inspect({ kind: "layout,slide,textbox,shape", maxChars: 12000 });
  console.log(`SOURCE: ${sourcePath}\n${report.ndjson}\n`);
}
