import hudson.model.*

//define logRotator rules

  final int DAYS_TO_KEEP   = 20
  final int NUMS_TO_KEEP = 20

  jobs = Hudson.instance.items

  ok_jobs = []
  wrong_jobs = []
  nolr_jobs = []

//sorting jobs

  sortJobs(jobs, DAYS_TO_KEEP, NUMS_TO_KEEP)
  
  makeTable(ok_jobs, "OK JOBS", 80)
  makeTable(wrong_jobs, "WRONG JOBS", 80)

// next code is brain dead

  print("Started healing process...")

  cureWrongJobs(wrong_jobs, DAYS_TO_KEEP, NUMS_TO_KEEP)

//  uncomment next string if you want to heal jobs which lacks logRotator (it will just add logRotator to each job)
//cureWrongJobs(nolr_jobs, DAYS_TO_KEEP, NUMS_TO_KEEP)


  println("Done!")
  println("After healing:")

  ok_jobs = []
  wrong_jobs = []
  nolr_jobs = []
  
  sortJobs(jobs, DAYS_TO_KEEP, NUMS_TO_KEEP)

  makeTable(ok_jobs, "OK JOBS", 80)
  makeTable(wrong_jobs, "WRONG JOBS", 80)
 
//supporting methods

  def sortJobs(jobs, days, nums) {
    jobs.each { job ->
      if (job.logRotator == null) {
        nolr_jobs.push(job)
      } else if (job.logRotator.daysToKeep > days || job.logRotator.numToKeep > nums) {
        wrong_jobs.push(job)
      } else if (job.logRotator.daysToKeep <= days && job.logRotator.numToKeep <= nums) {
        ok_jobs.push(job)
        }
    }
  }

  //be careful with jobNameMaxLength parameter
  def makeTable(list, caption, jobNameMaxLength) {
    println("")
    println("-" * 47)
    println("| $caption" + " " * (jobNameMaxLength - caption.length()) + "|" + " DAYS |" + " NUMS |" ) 
    println("-" * 47)
    list.each { job ->
      println("| $job.name" + " " * (jobNameMaxLength - job.name.length()) + "| $job.logRotator.daysToKeep" + " " * (5 -job.logRotator.daysToKeep.toString().length()) + "| $job.logRotator.numToKeep" + " " * (5 - job.logRotator.numToKeep.toString().length()) + "|")
    }
    println("-" * 47)
    println("")
  }

  def printJobList(list, caption) {
    println("")
    println("*** $caption ***")
    list.each {job ->
      println(job.name)
    }
    println("")
  }

  def cureWrongJobs(list, days, nums) {
    list.each { job -> 
      job.logRotator = new hudson.tasks.LogRotator(days, nums)
    }
  }
