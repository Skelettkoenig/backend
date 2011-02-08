class PreliminaryContestant < ActiveRecord::Base

  PROBS = ["Sicher", "Wahrscheinlich", "Vielleicht", "Eher nicht", "Sicher nicht"]

  has_one :contest, :through => :school
  belongs_to :school

  validates_presence_of :school, :name
  validates_uniqueness_of :name, :scope => :school_id 
  validates_inclusion_of :participation_probability, :in => PROBS

  def before_destroy
    Role.find(:all, :conditions => {:authorizable_type => "PreliminaryContestant", :authorizable_id => self.id}).each do |role|
      role.destroy
    end
  end

end