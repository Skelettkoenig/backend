class MatchesController < ApplicationController

  before_filter :fetch_parents

  access_control do
    allow :administrator

    action :show, :index_for_contestant do
      allow all
    end
  end

  def index
    @parent = Match.all

    respond_to do |format|
      format.html { redirect_to contest_matchday_url(@contest, @matchday, :anchor => "results")}
      format.xml  { render :xml => @matches }
    end
  end

  def show
    @match = @parent.matches.find(params[:id])

    respond_to do |format|
      format.html # show.html.erb
      format.xml  { render :xml => @match }
    end
  end

  protected

  def fetch_parents
    if params[:contest_id]
      @contest = Contest.find(params[:contest_id])
      @matchday = @contest.matchdays.find(params[:matchday_id])
      @parent = @matchday
    elsif params[:contestant_id]
      @contestant = Contestant.find(params[:contestant_id])
      @contest = @contestant.contest
      @parent = @contestant
    elsif params[:matchday_id]
      @matchday = current_contest.matchdays.find(params[:matchday_id])
    else
      raise ActiveRecord::RecordNotFound
    end
  end
end
