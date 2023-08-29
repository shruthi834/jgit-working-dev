package com.feature.JGit.controller;

import com.feature.JGit.entity.*;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@RestController
public class JGitController {
    @Value("${file.path.name}")
    private String filePath;
    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd  hh:mm:ss a");
    Logger log = LoggerFactory.getLogger(JGitController.class);
    @GetMapping("/getMapList")
    public String getMapList() {
        return "running";
    }
    @GetMapping("/gitLog")
    @ResponseBody
    public List<CommitDetails> getCommitDetails(@RequestParam String className) throws IOException {
        List<CommitDetails> commitDetailsList = new ArrayList<>();
        String repositoryPath = "H:\\repository\\ixmcm";
        String filePathVariable = filePath+className;
        Repository repository = Git.open(new File(repositoryPath)).getRepository();
        try {
            try (Git git = new Git(repository)) {
                Iterable<RevCommit> logs = git.log().addPath(filePathVariable).call();
                System.out.println("logs"+logs);
                for (RevCommit commit : logs) {
                    CommitDetails commitDetails = new CommitDetails();
                    commitDetails.setCommitId(commit.getId().getName());
                    commitDetails.setAuthor(commit.getAuthorIdent().getName());
                    commitDetails.setEmail(commit.getAuthorIdent().getEmailAddress());
                    commitDetails.setDate(commit.getAuthorIdent().getWhen().toString());
                    commitDetails.setMessage(commit.getFullMessage());
                    System.out.println("details"+commitDetails);
                    commitDetailsList.add(commitDetails);

                }
            }

        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return commitDetailsList;
    }
    @GetMapping("/modifiedFileList")
    @ResponseBody
    public List<ModifiedList> getModifiedFileList(@RequestParam String commitId) throws GitAPIException {
        List<ModifiedList> response = new ArrayList<>();
        ModifiedList modifiedFile =new ModifiedList();
        FileWithContent fileWithContent=new FileWithContent();
        String repositoryPath = "H:\\repository\\ixmcm";
        String filePathVariable="";
        try (Repository repository = Git.open(new File(repositoryPath)).getRepository()) {
            Git git = new Git(repository);
            ObjectId head = ObjectId.fromString(commitId);
            RevWalk rw = new RevWalk(repository);
            RevCommit revCommitId = rw.parseCommit(head);
            DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE);
            df.setRepository(repository);
            df.setDiffComparator(RawTextComparator.DEFAULT);
            df.setDetectRenames(true);
            int count = revCommitId.getParentCount();
            if(count > 0) {
                RevCommit parent = rw.parseCommit(revCommitId.getParent(0).getId());
                List<DiffEntry> diffs = df.scan(parent.getTree(), revCommitId.getTree());
                for (DiffEntry diff : diffs) {
//                    System.out.println("diffs"+diff);
                    filePathVariable = diff.getNewPath();
                    fileWithContent.setFilePath(filePathVariable);
                    ObjectLoader loader = repository.open(head);
                    String fileContent = new String(loader.getBytes());
                    fileWithContent.setFileContent(fileContent);
                    modifiedFile.setFiles(fileWithContent);
                    modifiedFile.setAuthor(String.valueOf(revCommitId.getAuthorIdent()));
                    modifiedFile.setCommitMessage(revCommitId.getFullMessage());
                    response.add(modifiedFile);
                }
            }
            else
            {

            }
        } catch (IOException  e) {
            e.printStackTrace();
        }
        return response;
    }
    @GetMapping("/commitFile")
    @ResponseBody
    public void getModifiedFileList() throws GitAPIException{
        String repositoryPath = "H:\\repository\\ixmcm";
        try (Repository repository = Git.open(new File(repositoryPath)).getRepository()) {
            Git git = new Git(repository);
            System.out.println("entered");
            String directoryPath ="H:\\repository\\ixmcm\\src\\main\\resources\\Draft_Maps";
            String fileName="testing.java";
            String data="Map details";
            String fullPath = directoryPath + "/" + fileName;
            FileOutputStream fos=new FileOutputStream(fullPath);
            byte[] byteData= data.getBytes();
            fos.write(byteData);
            fos.close();
//            System.out.println("src/main/resources/Draft_Maps/"+fileName);
            System.out.println("remote adding");
            git.remoteAdd()
                    .setName("origin/feature/jGit-feature")
                    .setUri(new URIish("https://stash.fhlbss.com/scm/col/ixmcm.git"))
                    .call();
//            git.add().addFilepattern("src/main/java/com/fhlbsf/ix/transformer/readme.txt").call();
            AddCommand addCommand = git.add();
            addCommand.addFilepattern("src/main/resources/Draft_Maps/"+fileName);
            addCommand.call();
            System.out.println("commiting");
            git.commit()
//                    .setOnly("src/main/java/com/fhlbsf/ix/transformer/FHLB_BankofSanFrancisco.java")
                    .setOnly("src/main/resources/Draft_Maps/"+fileName)
                    .setMessage("commit the file")
//                    .setMessage("commit the readme file")
                    .call();
            System.out.println("pushing");
            git.push()
                    .setRemote("https://stash.fhlbss.com/scm/col/ixmcm.git")
                    .setCredentialsProvider(
                            new UsernamePasswordCredentialsProvider("shruthie", "MondayFriday@123")
                    )
                    .call();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    @GetMapping("/listsFile")
    public List<FileDetails> getFileList() {
        List<FileDetails> fileDetailsList = new ArrayList<>();
        String repositoryPath = "H:\\dummy\\book-store-project";
        try(Repository repository = Git.open(new File(repositoryPath)).getRepository())  {

            Git git = new Git(repository);
            String folderPath="src/main/java/com/bittercode/model";
            ObjectId headId = repository.resolve("HEAD");
            RevWalk walk = new RevWalk(repository);
            RevCommit commit = walk.parseCommit(headId);
            RevTree tree = commit.getTree();
            System.out.println("Having tree: " + tree);
            // now use a TreeWalk to iterate over all files in the Tree recursively
            // you can set Filters to narrow down the results if needed
            TreeWalk treeWalk = new TreeWalk(repository);
            treeWalk.addTree(tree);
            treeWalk.setRecursive(true);
            while (treeWalk.next()) {
                String path = treeWalk.getPathString();
                if (path.startsWith(folderPath)) {
                    FileDetails fileDetails = new FileDetails();
                    String fileName = treeWalk.getNameString();
                    fileDetails.setId(UUID.randomUUID().toString());
                    fileDetails.setUserName(commit.getAuthorIdent().getName());
                    fileDetails.setMapName(fileName.substring(0,fileName.lastIndexOf('.')));
                    fileDetails.setCreateDate(new Date(commit.getCommitTime() * 1000L));
                    fileDetailsList.add(fileDetails);

                }
            }


        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return fileDetailsList;
    }

    @GetMapping("/fileContent")
    public void getFileContent(@RequestParam String commitId) {
        String repositoryPath = "H:\\repository\\ixmcm";
        try(Repository repository = Git.open(new File(repositoryPath)).getRepository()) {

//            Git git = new Git(repository);
            ObjectId commitObjectId = repository.resolve(commitId);
            try (RevWalk revWalk = new RevWalk(repository)) {
                RevCommit commit = revWalk.parseCommit(commitObjectId);
                RevTree tree = commit.getTree();
                CanonicalTreeParser treeParser = new CanonicalTreeParser();
                try (ObjectReader objectReader = repository.newObjectReader()) {
                    treeParser.reset(objectReader, tree.getId());

//                    while (!treeParser.eof()) {
                        String path = treeParser.getEntryPathString();
                        ObjectId objectId = treeParser.getEntryObjectId();
                        byte[] bytes = objectReader.open(objectId).getBytes();
                        String content = new String(bytes);
                        System.out.println("File path: " + path);
                        System.out.println("File content:\n" + content);
                        treeParser.next();
//                    }
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @GetMapping("/fetchDraftMapsFromStash1")
    public List<DraftMap> fetchDraftMapsFromStash1() {
        String path="src/main/resources/Draft_Maps";
        List<DraftMap> draftMapsList = new ArrayList<>();
//        ArrayList<RevCommit> commitList=new ArrayList<>();
        String repositoryPath = "H:\\repository\\ixmcm";
        try (Repository repository = Git.open(new File(repositoryPath)).getRepository()) {
            Git git=new Git(repository);
                Iterable<RevCommit>log=git.log().addPath(path).call();
                for(RevCommit commit :log) {
                    if(commit.getFullMessage().contains(commit.getAuthorIdent().getName())){
                        DraftMap map=new DraftMap(commit.getAuthorIdent().getName(),df.format(commit.getAuthorIdent().getWhen()), commit.getFullMessage());
                        draftMapsList.add(map);
                    }
                }
        }catch (Exception e) {
            log.error("Error Occurred file fetching the Draft Maps from Stash : "+e);
        }
        return draftMapsList;
    }

}
