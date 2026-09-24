import fs from "node:fs/promises";
import path from "node:path";
import { pathToFileURL } from "node:url";
import { Presentation, PresentationFile } from "@oai/artifact-tool";

const SKILL_DIR = "C:/Users/Arjun P Chandra/.codex/plugins/cache/openai-primary-runtime/presentations/26.904.11930/skills/presentations";
const workspaceDir = "C:/Users/Arjun P Chandra/Desktop/Training/Project";
const TMP_DIR = path.join(workspaceDir, ".build-grocers");
const FINAL_PPTX = path.join(workspaceDir, "output", "Online_Grocers_Project_Presentation_Final.pptx");
const RUNTIME_PYTHON = "C:/Users/Arjun P Chandra/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe";
const { resolvePresentationFont, finalizePresentation } = await import(pathToFileURL(path.join(SKILL_DIR, "container_tools/artifact_tool_utils.mjs")).href);
const font = resolvePresentationFont({ fontFamily: "Arial" });
const pres = Presentation.create({ slideSize: { width: 1280, height: 720 } });

const C = { red: "#C74634", wine: "#3B1012", ink: "#1A1716", muted: "#5D5854", sand: "#F6F1EC", peach: "#F2DED7", white: "#FFFFFF", line: "#DCCFC7", green: "#2D735D" };
function box(slide, x, y, w, h, fill, radius = "rect") { return slide.shapes.add({ geometry: radius, position: { left:x, top:y, width:w, height:h }, fill, line:{ fill:"none", width:0 } }); }
function text(slide, value, x, y, w, h, size=20, color=C.ink, bold=false, align="left") {
  const s = slide.shapes.add({ geometry:"textbox", position:{left:x,top:y,width:w,height:h}, fill:"none", line:{fill:"none",width:0} });
  s.text = value;
  s.text.style = { typeface:font, fontSize:size, color, bold, autoFit:"shrinkText", paragraphAlignment:align, verticalAlignment:"middle", marginLeft:0, marginRight:0, marginTop:0, marginBottom:0 };
  return s;
}
function title(slide, value, kicker="ONLINE GROCERS") { text(slide,kicker,72,42,550,18,12,C.red,true); text(slide,value,72,69,1070,52,32,C.ink,true); box(slide,72,134,70,4,C.red); }
function footer(slide, n) { text(slide,"Grocers Project  |  Team 1",72,682,400,16,10,C.muted,false); text(slide,String(n).padStart(2,"0"),1160,682,48,16,10,C.muted,true,"right"); }
function bullets(slide, items, x, y, w, size=18, color=C.ink, gap=40) { items.forEach((item,i)=>{ box(slide,x,y+i*gap+9,8,8,C.red,"ellipse"); text(slide,item,x+20,y+i*gap,w-20,30,size,color,false); }); }
function note(slide, value) { slide.speakerNotes.textFrame.setText(value); }
function contribution(slide, n, left, right=null) {
  title(slide,"Team Contributions","TEAM");
  const make = (person, x, width) => {
    box(slide,x,164,width,430,C.sand);
    box(slide,x+34,206,130,130,"#E5D6CF");
    text(slide,"PHOTO\nPLACEHOLDER",x+42,251,114,40,10,C.muted,true,"center");
    text(slide,person,x+190,205,width-220,34,23,C.ink,true);
    text(slide,"Major contributions",x+190,270,width-220,20,14,C.red,true);
    text(slide,"[To be confirmed]",x+190,299,width-220,28,17,C.ink,false);
    text(slide,"Partial contributions",x+190,371,width-220,20,14,C.red,true);
    text(slide,"[To be confirmed]",x+190,400,width-220,28,17,C.ink,false);
  };
  if (right) { make(left,72,540); make(right,668,540); } else { make(left,280,720); }
  footer(slide,n);
}

// 1: cover
{ const s=pres.slides.add(); s.background.fill=C.wine; box(s,0,0,1280,720,C.wine); box(s,72,103,84,5,C.red); text(s,"ONLINE GROCERS",72,142,700,66,48,C.white,true); text(s,"A web application for grocery shopping, inventory control, and order operations",72,222,590,62,23,"#F2DDD7",false); text(s,"Project Presentation",72,337,300,24,16,C.white,true); text(s,"Team 1  |  September 2026",72,369,390,24,15,"#F2DDD7",false); box(s,804,108,350,350,C.red,"ellipse"); box(s,856,160,246,246,"#E5A296","ellipse"); text(s,"GROCERS",863,258,232,40,28,C.wine,true,"center"); text(s,"Fresh orders\nClear operations\nReliable fulfillment",830,498,360,76,20,C.white,false,"center"); text(s,"Source: Project-Grocers.pdf",72,678,300,14,10,"#F2DDD7",false); note(s,"Source: Project-Grocers.pdf, page 1."); }
// 2: overview
{ const s=pres.slides.add(); s.background.fill=C.white; title(s,"Project Overview"); text(s,"Objective",72,180,200,30,18,C.red,true); text(s,"Create a dynamic online grocery platform that supports purchasing, administration, and inventory operations.",72,222,480,90,24,C.ink,true); box(s,655,176,480,310,C.sand); text(s,"Core scope",692,210,240,24,18,C.red,true); bullets(s,["REST APIs with a modern web frontend","Admin, User, and Employee portals","Database-backed products, accounts, orders, and requests","Single-page experience after sign-in"],692,252,385,17,C.ink,48); text(s,"Suggested technology stack",72,463,280,24,18,C.red,true); text(s,"Spring Boot or JAX-RS  /  React.js or Angular  /  Node.js  /  Database",72,506,1040,36,20,C.ink,false); footer(s,2); note(s,"Source: Project-Grocers.pdf, page 1 and page 8."); }
// 3 roles
{ const s=pres.slides.add(); s.background.fill=C.white; title(s,"Platform Roles and Responsibilities"); const roles=[['ADMIN','Catalog, employees, requests, reports'],['USER','Browse, cart, checkout, orders, funds'],['EMPLOYEE','Inventory requests, order updates, unlocks']]; roles.forEach((r,i)=>{let x=72+i*378; box(s,x,194,320,260,i===0?C.wine:(i===1?C.sand:"#EAF0ED")); text(s,r[0],x+30,231,260,32,24,i===0?C.white:C.ink,true); box(s,x+30,286,64,4,C.red); text(s,r[1],x+30,323,246,78,18,i===0?"#F2DDD7":C.ink,false);}); text(s,"The application routes each authenticated user to a role-specific dashboard.",72,510,900,28,20,C.ink,true); footer(s,3); note(s,"Source: Project-Grocers.pdf, pages 3-8."); }
// 4 customer
{ const s=pres.slides.add(); s.background.fill=C.white; title(s,"User Shopping Experience"); const steps=[['1','Register and sign in'],['2','Browse and manage cart'],['3','Add funds and checkout'],['4','Track order status']]; steps.forEach((d,i)=>{let x=72+i*285; text(s,d[0],x,194,48,48,34,C.red,true); box(s,x,258,218,2,C.line); text(s,d[1],x,284,230,54,19,C.ink,true);}); box(s,72,414,1040,112,C.sand); text(s,"Key controls",104,440,150,22,16,C.red,true); text(s,"Lock accounts after three unsuccessful sign-in attempts. Customers can raise a ticket for an employee to review and unlock the account.",104,472,920,34,18,C.ink,false); footer(s,4); note(s,"Source: Project-Grocers.pdf, pages 5-7."); }
// 5 admin
{ const s=pres.slides.add(); s.background.fill=C.white; title(s,"Admin Portal"); box(s,72,179,440,360,C.wine); text(s,"Control center",108,218,240,30,22,C.white,true); bullets(s,["Add, update, and delete products","Manage employee accounts","Review inventory requests","Generate order reports"],108,275,330,18,C.white,48); box(s,592,179,520,360,C.sand); text(s,"Reporting options",628,218,280,30,22,C.ink,true); bullets(s,["Daily, weekly, and monthly reports","Product-specific report view","Customer-specific report view","Secure admin sign-in and logout"],628,275,380,18,C.ink,48); footer(s,5); note(s,"Source: Project-Grocers.pdf, pages 3-4."); }
// 6 employee
{ const s=pres.slides.add(); s.background.fill=C.white; title(s,"Employee Operations"); text(s,"Employees keep inventory and fulfillment moving after secure sign-in and first-login password change.",72,171,1000,28,20,C.ink,true); const ops=[['Inventory requests','Ask the admin to replenish or adjust products.'],['Order status','Set shipped, out for delivery, delivered, or cancelled.'],['Account support','Review tickets and unlock eligible user accounts.']]; ops.forEach((o,i)=>{let y=255+i*98; box(s,72,y,1040,72,i===1?"#F7E7E1":C.sand); text(s,o[0],104,y+18,210,26,18,C.red,true); text(s,o[1],344,y+18,700,26,17,C.ink,false);}); footer(s,6); note(s,"Source: Project-Grocers.pdf, pages 7-8."); }
// 7 architecture
{ const s=pres.slides.add(); s.background.fill=C.white; title(s,"Proposed Application Architecture"); const layers=[['Web frontend','Role-based single-page portal'],['REST API layer','Authentication, catalog, cart, order, ticket, and reporting services'],['Database','Users, employees, products, orders, funds, and requests']]; layers.forEach((l,i)=>{let y=184+i*134; box(s,160,y,790,92,i===1?C.wine:C.sand); text(s,l[0],194,y+22,210,25,18,i===1?C.white:C.red,true); text(s,l[1],430,y+22,480,32,17,i===1?C.white:C.ink,false); if(i<2) text(s,"↓",540,y+96,40,30,25,C.red,true,"center");}); text(s,"Design principle",1002,226,160,20,15,C.red,true); text(s,"Separate role portals share the same service and data foundation.",1002,262,180,96,18,C.ink,true); footer(s,7); note(s,"Source: Project-Grocers.pdf, page 1. Architecture is a proposed implementation based on the stated technology requirements."); }
// 8 business rules
{ const s=pres.slides.add(); s.background.fill=C.white; title(s,"Business Rules and Validation"); const rules=[['Authentication','Validate credentials and show clear error messages.'],['Account protection','Lock user access after three failed sign-in attempts.'],['Checkout','Place an order only when available funds cover the cart total.'],['Cancellation','Refund funds and show the reason when an employee cancels an order.'],['Data quality','Generate primary keys and validate required form inputs.']]; rules.forEach((r,i)=>{let y=172+i*72; text(s,r[0],72,y,220,24,17,C.red,true); text(s,r[1],326,y,790,24,17,C.ink,false); box(s,72,y+47,1038,1,C.line);}); footer(s,8); note(s,"Source: Project-Grocers.pdf, pages 3-8."); }
// 9 delivery
{ const s=pres.slides.add(); s.background.fill=C.white; title(s,"Delivery Focus"); text(s,"The first release should cover every required use case before optional enhancements.",72,171,940,28,20,C.ink,true); const items=[['Foundation','Data model, authentication, and role routing'],['Core commerce','Products, cart, funds, checkout, and orders'],['Operations','Inventory requests, reports, tickets, and user unlocking'],['Quality','Validation, error messages, and responsive interface']]; items.forEach((d,i)=>{let x=72+(i%2)*540,y=250+Math.floor(i/2)*156; box(s,x,y,490,112,C.sand); text(s,d[0],x+28,y+22,200,24,19,C.red,true); text(s,d[1],x+28,y+56,415,30,17,C.ink,false);}); footer(s,9); note(s,"Source: Project-Grocers.pdf, pages 1 and 8. Delivery sequence is a proposed presentation summary."); }
contribution(pres.slides.add(),10,"Akash SS","Arjun P Chandra");
contribution(pres.slides.add(),11,"Karugapuram Abhiram","Pragathi Sanjeev Acharya");
contribution(pres.slides.add(),12,"Pranav Patil");

await fs.mkdir(path.join(workspaceDir,".codex-finalizer"),{recursive:true});
await fs.mkdir(path.dirname(FINAL_PPTX),{recursive:true});
const candidatePath = path.join(workspaceDir,".codex-finalizer","grocers-candidate-final.pptx");
await (await PresentationFile.exportPptx(pres)).save(candidatePath);
await finalizePresentation({ workspaceDir, candidatePath, finalPath:FINAL_PPTX, pythonExecutable:RUNTIME_PYTHON, integrityValidatorPath:path.join(SKILL_DIR,"container_tools/inspect_presentation_package_integrity.py"), layoutValidatorPath:path.join(SKILL_DIR,"container_tools/inspect_presentation_layout_geometry.py"), layoutArgs:["--expected-slide-size-emu","12192000,6858000","--validate-bullet-geometry","--validate-heading-fit"], fontPolicy:{basis:"design",families:[font]}, verifyArtifactToolImport:true, receiptPath:path.join(workspaceDir,".codex-finalizer","grocers-final.validation.json") });
console.log(FINAL_PPTX);
