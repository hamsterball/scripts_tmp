require 'rugged'
require 'getoptlong'

  opts = GetoptLong.new(
    [ '--needful', '-n', GetoptLong::REQUIRED_ARGUMENT ],
    [ '--junk', '-j', GetoptLong::REQUIRED_ARGUMENT ],
    [ '--repopath', '-r', GetoptLong::REQUIRED_ARGUMENT ]
  )

  @needful_branch_name = nil
  @junk_branch_name = nil 
  @repo_path = nil

  opts.each do |opt, arg|
    case opt
      when '--needful'
        @needful_branch_name = arg
      when '--junk'
        @junk_branch_name = arg
      when '--repopath'
        @repo_path = arg
    end
  end

  @repo = Rugged::Repository.new(@repo_path)
  @junk_branch = Rugged::Branch.lookup(@repo, @junk_branch_name)
  @needful_branch = Rugged::Branch.lookup(@repo, @needful_branch_name)

  def p_branches f
    Rugged::Branch.each(@repo, filter = f) do |branch|
      puts branch.name
    end
  end

  def remove_junk 
    if !@junk_branch.head? && !@junk_branch.nil?
      @junk_branch.delete!
      puts "Junk branch removed!"
    elsif @junk_branch.nil?
      puts "Junk branch doesn't exists"
      exit 0
    else
      puts "Junk branch is HEAD, can't delete it"
      exit 0
    end
  end

  def copy_needful
      Rugged::Branch.create(@repo, @junk_branch_name, @needful_branch_name)
      puts "New junk branch created!"
  end

  remove_junk
  copy_needful

  p_branches :local
